import { Component, OnInit } from '@angular/core';
import { Sort } from '@angular/material/sort';
import { AgentService } from '../service/agent.service';

@Component({
  selector: 'app-running-agents',
  templateUrl: './running-agents.component.html',
  styleUrls: ['./running-agents.component.css']
})
export class RunningAgentsComponent implements OnInit {

  constructor(public agentService: AgentService) { }

  ngOnInit(): void {
    this.getRunningAgents();
  }

  getRunningAgents() {
    this.agentService.getRunning();
  }

  sortData(sort: Sort) {
    const data = this.agentService.runningAgents.slice();
    if (!sort.active || sort.direction === '') {
      this.agentService.runningAgents = data;
      return;
    }

    this.agentService.runningAgents = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'name': return compare(a.name, b.name, isAsc);
        case 'alias': return compare(a.type.host.alias, b.type.host.alias, isAsc);
        case 'address': return compare(a.type.host.address, b.type.host.address, isAsc);
        case 'type': return compare(a.type.name, b.type.name, isAsc);
        default: return 0;
      }
    });
  }
}

function compare(a: number | string, b: number | string, isAsc: boolean) {
  return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}